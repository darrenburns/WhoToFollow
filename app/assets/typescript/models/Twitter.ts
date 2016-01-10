
module Twitter {

    interface RetweetedUser {
        username: string;
        screenname: string;
    }

    /**
     * Status returned from the getUserTimeline Twitter API call.
     */
    export interface Status {
        id: number
        text: string
        username: string
        screenname: string
        date: number
        retweets: number
        likes: number
        avatar: string  // The URL for the user's avatar
        isRetweet: boolean;
        retweetedUser?: RetweetedUser
    }

}

