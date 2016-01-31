///<reference path='../models/Learning.ts'/>

import * as $ from 'jquery';
import Configuration from '../util/config';

export default class LearningUtils {

    public static markUserAsRelevant(screenName: string,
                           query: string,
                           isRelevant: boolean): void {
        let endpoint = "learning/rate-user";
        let xhr: JQueryXHR = $.ajax(Configuration.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "query": query,
                "isRelevant": isRelevant
            }),
            contentType: "application/json"
        });
        xhr.then(
            doneResponse => {
                console.log("User rated.");
            },
            failResponse => {
                console.log("Failed to rate user.");
            }
        )
    }

    public static getUserRelevance(screenName: string): JQueryXHR {
        let endpoint = "learning/get-user-relevance";
        return $.get(Configuration.BASE_SITE_URL + endpoint, {
            data: JSON.stringify({
                screenName: screenName
            })
        })
    }

}