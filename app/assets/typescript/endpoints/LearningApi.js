///<reference path='../models/Learning.ts'/>
var $ = require('jquery');
var config_1 = require('../util/config');
var LearningUtils = (function () {
    function LearningUtils() {
    }
    LearningUtils.markUserAsRelevant = function (screenName, query, isRelevant) {
        var endpoint = "learning/rate-user";
        var xhr = $.ajax(config_1.default.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "query": query,
                "isRelevant": isRelevant
            }),
            contentType: "application/json"
        });
        xhr.then(function (doneResponse) {
            console.log("User rated.");
        }, function (failResponse) {
            console.log("Failed to rate user.");
        });
    };
    LearningUtils.getUserRelevance = function (screenName) {
        var endpoint = "learning/get-user-relevance";
        return $.get(config_1.default.BASE_SITE_URL + endpoint, {
            data: JSON.stringify({
                screenName: screenName
            })
        });
    };
    return LearningUtils;
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = LearningUtils;
