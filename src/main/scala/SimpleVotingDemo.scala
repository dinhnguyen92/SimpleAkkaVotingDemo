import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object SimpleVotingDemo extends App {

  object VotingSystem {
    // Message to request vote aggregate to print the result
    case object VotingResult

    // Message sent to a voter by vote aggregate to request the voter's vote
    case object VoteRequest

    // Message sent to tell a voter to vote for a particular candidate
    case class Vote(candidate: String)

    // Message from the voter to tell the vote aggregator of her selected candidate
    case class VoteReply(candidate: Option[String])

    // Message sent to tell the vote aggregator to start collecting votes from the specified voters
    case class AggregateVotes(voters: Set[ActorRef])
  }

  class Voter extends Actor {
    import VotingSystem._

    override def receive: Receive = receiveVotingMsg(None)

    def receiveVotingMsg(selectedCandidate: Option[String]): Receive = {
      // Each voter can only vote once
      // Once a voter has voted for a candidate, i.e. selectedCandidate is no longer empty
      // they can no longer change their vote
      case Vote(candidate) if (selectedCandidate.isEmpty) => context.become(receiveVotingMsg(Some(candidate)))

      // When receiving a request for vote from the vote aggregator
      // Send back the selected candidate
      case VoteRequest => sender ! VoteReply(selectedCandidate)
    }
  }

  class VoteAggregator extends Actor {
    import VotingSystem._

    // Before sending out vote requests to the voters
    // The vote aggregator will not entertain requests to count, submit, or print votes
    // Hence the initial partial function will only handle AggregateVotes messages
    override def receive: Receive = {
      case AggregateVotes(voters) =>
        // Send vote requests to all voters
        voters.foreach(_ ! VoteRequest)

        // 1. Initialize empty map to keep track of vote count
        // 2. Put all voters into the set of voters who haven't voted
        // 3. Change the actor's behavior to count votes
        context.become(countVotes(Map[String, Int](), voters))
    }

    // Once requests for vote have been sent
    // The aggregate will only count, receive, or print votes
    // Hence the partial function will not handle AggregateVotes messages
    def countVotes(poll: Map[String, Int], yetToVote: Set[ActorRef]): Receive = {

      // If a voter hasn't voted yet, keep requesting vote from that voter
      case VoteReply(None) => sender ! VoteRequest

      case VoteReply(Some(candidate))=>
        // Increment the vote count of the candidate
        val count = poll.getOrElse(candidate, 0) + 1
        val updatedPoll = poll + (candidate -> count)

        // Save the updated poll and remove the voter from the list of voters who haven't voted
        context.become(countVotes(updatedPoll, yetToVote - sender))

      case VotingResult =>
        // If there are still voters who haven't voted
        // Keep forwarding request for voting result to self
        // The forwarded requests effectively create a loop
        // that will continue until all voters have voted
        // Once all voters have voted, print the poll
        if (yetToVote.nonEmpty) self forward VotingResult
        else for ((candidate, count) <- poll) println(s"${candidate}: ${count} votes")
    }
  }

  import VotingSystem._

  val system = ActorSystem("ActorSystem")
  val alice = system.actorOf(Props[Voter], "Alice")
  val bob = system.actorOf(Props[Voter], "Bob")
  val charlie = system.actorOf(Props[Voter], "Charlie")
  val daniel = system.actorOf(Props[Voter], "Daniel")
  val voteAggregator = system.actorOf(Props[VoteAggregator])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
  voteAggregator ! VotingResult
}
