# WhoToFollow In Context
Recommending Twitter users to follow for information about events.

## Status
This is a **work in progress repository**for the **Individual Project** Level 4 course at The University
of Glasgow.

## Build instructions

The Scala portion of this project can be built using [sbt](http://www.scala-sbt.org).
The build tool used for the front-end is [Gulp](http://gulpjs.com).

 1. `git clone` this repository.
 2. `cd WhoToFollow/app/assets/js` - Go the the front-end project root.
 3. `npm install` - Install JavaScript dependencies.
 4. `gulp build` - Transpile and concatenate JavaScript (other Gulp tasks are available).
 5. `cd ../../../` - Return to the project root
 6. `sbt run` - Get dependencies, compile the backend code and run the server. Auto-reloading is enabled.

## Milestones

- [x] Initial literature and research
- [x] Create project skeleton
- [x] Channel (results-streaming) system
- [ ] Define, configure and store in database
- [ ] Query handler initialisation
- [ ] Determining user expertise in event
- [ ] Displaying expertise ranking to user
- [ ] Analysis of tweet quality
- [ ] Analysis of link quality
