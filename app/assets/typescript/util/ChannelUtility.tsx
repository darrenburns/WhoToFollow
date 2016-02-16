import Constants from './constants';

export default class ChannelUtility {

    public static buildKeepAlive(channel: string): string {
        return JSON.stringify({
            "channel": channel,
            "request": Constants.KEEP_ALIVE_STRING
        });

    }

}