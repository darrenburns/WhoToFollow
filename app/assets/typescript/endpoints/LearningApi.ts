import * as $ from 'jquery';
import Configuration from '../util/config';

export default class LearningUtils {

    public static markUserAsRelevant(screenName: string,
                           query: string,
                           isRelevant: boolean): JQueryXHR {
        let endpoint = "learning/rate-user";
        return $.ajax(Configuration.BASE_SITE_URL + endpoint, {
            method: "POST",
            data: JSON.stringify({
                "screenName": screenName,
                "query": query,
                "isRelevant": isRelevant
            }),
            contentType: "application/json"
        });
    }

    public static getUserRelevance(screenName: string, query: string): JQueryXHR {
        let endpoint = "learning/get-user-relevance";
        return $.get(Configuration.BASE_SITE_URL + endpoint, {screenName: screenName, query: query})
    }

}