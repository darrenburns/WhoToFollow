
module Twitter {

    /**
     * Status returned from the getUserTimeline Twitter API call.
     */
    export interface Status {
        id: number
        text: string
        username: string
        screenname: string
        date: Date
        retweets: number
        avatar: string  // The URL for the user's avatar
    }

}

