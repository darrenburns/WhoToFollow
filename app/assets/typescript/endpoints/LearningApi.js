///<reference path='../models/Learning.ts'/>
var $ = require('jquery');
var config_1 = require('../util/config');
var LearningUtils = (function () {
    function LearningUtils() {
    }
    LearningUtils.classifyUser = function (screenName, query, clas) {
        var endpoint = "learning/classify";
        var xhr = $.ajax(config_1.default.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "hashtag": query,
                "voteId": clas
            }),
            contentType: "application/json"
        });
        xhr.then(function (doneResponse) {
            console.log("Server successfully classified user.");
        }, function (failResponse) {
            console.log("Server failed at classifying user.");
        });
    };
    return LearningUtils;
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = LearningUtils;
