
module Learning {
    export enum QualityClass {
        LOW, HIGH
    }

    /*

     */
    export interface UserFeatures {
        screenName: string;
        tweetCount: number;
        followerCount: number;
        wordCount: number;
        capitalisedCount: number;
        hashtagCount: number;
        retweetCount: number;
        likeCount: number;
        dictionaryHits: number;
        linkCount: number;
    }
}