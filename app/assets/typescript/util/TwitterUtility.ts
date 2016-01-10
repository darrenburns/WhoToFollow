
export default class TwitterUtility {

    public static getProfilePictureUrl(screenName: string, sizeStr: string): string {
        return `https://twitter.com/${screenName}/profile_image?size=${sizeStr}`;
    }

}