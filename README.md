# WhoToFollow In Context
Recommending Twitter users to follow for information about events.

## Build instructions

The Scala portion of this project can be built using [sbt](http://www.scala-sbt.org).
The build tool used for the front-end is [Gulp](http://gulpjs.com).

 1. `git clone` this repository.
 2. `cd WhoToFollow/app/assets/typescript` - Go the the front-end project root.
 3. `npm install` - Install front-end dependencies.
 4. `tsd install` - Fetch TypeScript definition files.
 5. `gulp less` - Compile and bundle the LESS files.
 6. `gulp build` - Transpile and concatenate TypeScript.
 7. `cd ../../../` - Return to the project root
 8. `activator run` - Get server dependencies, compile the backend code and run the server. Auto-reloading is enabled.

