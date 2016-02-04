var $ = require('jquery');
var config_1 = require('../util/config');
var LearningUtils = (function () {
    function LearningUtils() {
    }
    LearningUtils.markUserAsRelevant = function (screenName, query, isRelevant) {
        var endpoint = "learning/rate-user";
        return $.ajax(config_1.default.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "query": query,
                "isRelevant": isRelevant
            }),
            contentType: "application/json"
        });
    };
    LearningUtils.getUserRelevance = function (screenName, query) {
        var endpoint = "learning/get-user-relevance";
        return $.get(config_1.default.BASE_SITE_URL + endpoint, { screenName: screenName, query: query });
    };
    return LearningUtils;
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = LearningUtils;
