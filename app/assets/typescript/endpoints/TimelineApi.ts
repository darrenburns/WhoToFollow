import * as $ from 'jquery';
import Configuration from '../util/config';


export default class TimelineApi {

    public static fetchAndAnalyse(screenName: string): JQueryXHR {
        let endpoint = "learning/timeline/" + screenName;
        return $.ajax(
            Configuration.BASE_SITE_URL + endpoint
        )
    }

}