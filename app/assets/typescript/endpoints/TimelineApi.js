"use strict";
var $ = require('jquery');
var config_1 = require('../util/config');
var TimelineApi = (function () {
    function TimelineApi() {
    }
    TimelineApi.fetchAndAnalyse = function (screenName) {
        var endpoint = "learning/timeline/" + screenName;
        return $.ajax(config_1.default.BASE_SITE_URL + endpoint);
    };
    return TimelineApi;
}());
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = TimelineApi;
