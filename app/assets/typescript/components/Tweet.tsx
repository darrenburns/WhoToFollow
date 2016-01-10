import * as React from 'react';
import * as moment from 'moment';
import {FontIcon} from 'material-ui';
import TwitterUtility from '../util/TwitterUtility';

interface IRetweetedUser {
    username: string;
    screenName: string;
}

interface ITweetProps {
    key: number;
    query: string;
    colour: string;
    status: Twitter.Status;
}


export default class Tweet extends React.Component<ITweetProps, any> {

    private static highlightQuery(query: string, text: string): string {
        let regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, `<mark>$1</mark>`);
    }

    render () {
        let tweetFontIconStyles = {
            color: '#b4b4b4',
            marginRight: '3px'
        };
        let status = this.props.status;
        let username = status.isRetweet ? status.retweetedUser.username : status.username;
        let screenName = status.isRetweet ? status.retweetedUser.screenname : status.screenname;
        let statusText = status.isRetweet ? status.text.substring(3) : status.text;
        return (
            <div className="tweet">
                <div className="tweet-sidebar-left">
                    <img src={TwitterUtility.getProfilePictureUrl(screenName, "normal")} alt={screenName} width="48px" height="48px"/>
                </div>
                <div className="tweet-body">
                    <strong>{username}</strong> - <span style={{color: `#${this.props.colour}`}}>@{screenName}</span>
                    <br/>
                    <span className="tweet-text" dangerouslySetInnerHTML={{__html: Tweet.highlightQuery(this.props.query, statusText)}}>
                        {/*Highlighted tweet is injected here.*/}
                    </span>
                    <br/>
                    <span className="tweet-subtext">
                        <em>
                            {moment(this.props.status.date).fromNow()}
                        </em>
                        <em>
                            {status.isRetweet ? `(RT by ${status.username})` : '' }
                        </em>
                    </span>
                </div>
                <div className="tweet-sidebar-right">
                    <span className="tweet-likes">
                        <FontIcon className="material-icons" style={tweetFontIconStyles}>favorite_border</FontIcon> <strong>{this.props.status.likes}</strong>
                    </span>
                    <span className="tweet-retweets">
                        <FontIcon className="material-icons" style={tweetFontIconStyles}>cached</FontIcon> <strong>{this.props.status.retweets}</strong>
                    </span>
                </div>
            </div>
        )
    }

}
