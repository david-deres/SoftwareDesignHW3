# SifriTaub • Assignment 3

## Authors
* David Deres
* Adan Abo Hyeah

## Notes

Library used: 316278837-322252685

### Implementation Summary
Well, as requested, we chose a library written by other students - it wraps SecureStorage and requires a binding for 
SecureStorageFactory and a name.
We used it to create our persistent databases:
    - Users DB : storing existent users with their password, used by UsersManager
    - Inboxes DB: maps between username to its inbox, used by MessagingHandler
    - IdsToUsersDB: as its name suggests, used for finding the right sender by message id, for use in deleteMessage by
      MessagingHandler.
Overall structure and classes:
- MessagingClientManager is the MessagingClientFactory that creates clients. also, it creates UsersManager instance and
    MessagingHandler instance once, and creates client with those instances to gain persistence and context for all the
    clients created.
- MessagingHandler is the class that holds the inbox DB's, and those responsible for supplying and editing them.
- UsersManager is the class responsible for managing existing users and managing the OnlineUsers list, 
   which is in memory list, since it's not persistent 
   (new ClientManager will create new UsersManager and the list will be empty)

There are also classes created by protobuf: MessageP, InboxP and Messagelist - see app/src/proto for definition

### Testing Summary
As usual, we first implemented the simplest tests according to the documentation of the MessagingClient - 
Exceptions and basic functionality. Next, we thought about some edge cases and some use cases which requires integration
between several methods and components iin the code to see that all the communication and serialization works correctly.

### Difficulties
The most challenging one was to figure out how to properly use (and in particular configure) Protobuf,
since it's not that mature for use with Kotlin
(not a lot of info in the net, except Google's developers official guide).
Also, we had to think how to properly serialize and deserialize Inbox since it's nested - then Protobuf came handy.

Another challenge was to figure out how to work with the libraries supplied by others - we guess it's common to
think that yours implementation is the best and to question every line of code somebody else wrote (although there were
very elegant and sophisticated libraries )

### Feedback
Cool idea of using others library, also a good way to really understand the need for good Serialization framework.