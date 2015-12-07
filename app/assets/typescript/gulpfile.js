var gulp = require('gulp');
var sourcemaps = require('gulp-sourcemaps');
var source = require('vinyl-source-stream');
var buffer = require('vinyl-buffer');
var browserify = require('browserify');
var watchify = require('watchify');
var babel = require('babelify');
var less = require('gulp-less');
var path = require('path');
var tsify = require('tsify');
var walker = require('walk-sync');


function compile(watch) {
    var bundler = watchify(
        browserify('./components/App.tsx', { debug: true }));

    walker('typings').forEach(function(file) {
        if (file.match(/\.d\.ts$/)) {
            bundler.add("typings/" + file);
        }
    });

    bundler.plugin(tsify, { noImplicitAny: false });

    function rebundle() {
        bundler.bundle()
            .on('error', function(err) { console.error(err); })
            .pipe(source('build.js'))
            .pipe(buffer())
            .pipe(sourcemaps.init({ loadMaps: true }))
            .pipe(sourcemaps.write('./'))
            .pipe(gulp.dest('../../../public/javascripts'));
    }

    if (watch) {
        bundler.on('update', function() {
            console.log('-> Compiling TypeScript sources...');
            rebundle();
            console.log('-> Finished compiling TypeScript sources.')
        });
    }

    rebundle();
}


function watch() {
    return compile(true);
}

gulp.task('less', function () {
    return gulp.src('../less/**/*.less')
        .pipe(less({
            paths: [ path.join(__dirname, 'less', 'includes') ]
        }))
        .pipe(gulp.dest('../../../public/stylesheets'));
});
gulp.task('build', function() { return compile(); });
gulp.task('watch', function() { return watch(); });

gulp.task('default', ['watch']);