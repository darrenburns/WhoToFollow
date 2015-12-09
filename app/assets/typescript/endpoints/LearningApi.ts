///<reference path='../models/Learning.ts'/>

import * as $ from 'jquery';
import Configuration from '../util/config';

export default class LearningUtils {

    public static classifyUser(screenName: string,
                               query: string,
                               clas: Learning.QualityClass): void {
        let endpoint = "learning/classify";
        let xhr: JQueryXHR = $.ajax(Configuration.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "hashtag": query,
                "voteId": clas
            }),
            contentType: "application/json"
        });
        xhr.then(
            doneResponse => {
                console.log("Server successfully classified user.");
            },
            failResponse => {
                console.log("Server failed at classifying user.");
            }
        )

    }

}