# SimpleAkkaVotingDemo
This is the solution to the Akka actor voting exercise in the course "Akka Essentials with Scala" taught by Daniel Ciocîrlan on Udemy. The purpose of this exercise is to simulate a very simple voting system that consists of 2 types of Akka actors:

1. Voter: upon receiving a vote messsage from the main thread, a voter will "vote" for the candidate specified in the message. Once the voter has selected a candidate, the choice must be persistent and cannot be changed.

2. Vote aggregator: the vote aggregator is responsible for aggregating the voting choices of multiple voters and for printing the results when prompted by the main thread.

Both the voters and the vote aggregator are implemented as fully stateless and immutable Akka actors. Instead of using mutable state variables, the "states" of the actors (selected candidate in the case of voters; vote count and voter list in the case the vote aggregator) are persisted through the use of behavior changes and state parameters that are passed between behavior partial functions. The behaviors of the actors are broken down into small partial functions to keep the communication logic between the actors simple and intuitive. 

The main thread, the voters, and the vote aggregator communicate by sending asynchronous and non-blocking messages. The main steps in the program are:

1. The main thread sends messages to tell the voters to vote

2. The vote aggregator sends messages to the voters to collect their vote choices

3. The main thread sends a message to tell the vote aggregator to print the poll results

Since communication between the main thread, the voters, and the vote aggregator is asynchronous, a small trick is used to "synchronize" the aggregate's printing of the results with the main thread. Upon receiving the result request from the main thread, if the aggregator has yet to collect the voting choices of all voters, it will simply forward the request to itself without processing it. This effectively creates a loop in which the aggregator "waits" for all of the voters to vote. This trick allows the main thread to send a result request at a time when not all of the votes have come back and still obtain the complete poll results. Without this trick, if the main thread sends the result request while some voters still haven't submitted their votes, it will receive only the partial poll results from the vote aggregator.

Credits go to Daniel Ciocîrlan for coming up with this exercise, which demonstrates Akka actor's behaviors and messaging in a very intuitive way.
