import * as React from 'react';
import * as moment from 'moment';
import {FontIcon} from 'material-ui';

interface ITweetProps {
    key: number;
    likes: number;
    retweets: number;
    timestamp: number;
    text: string;
}


export default class Tweet extends React.Component<ITweetProps, any> {

    render () {
        let tweetFontIconStyles = {
            color: '#878787',
            marginRight: '3px'
        };
        return (
            <div className="tweet" key={this.props.key}>
                <div className="tweet-body">
                    <span className="tweet-text">
                        {this.props.text}
                    </span>
                    <br/>
                    <span className="tweet-subtext">
                        <em>
                            {moment(this.props.timestamp).fromNow()}
                        </em>
                    </span>
                </div>
                <div className="tweet-sidebar">
                    <span className="tweet-likes">
                        <FontIcon className="material-icons" style={tweetFontIconStyles}>favorite</FontIcon> <strong>{this.props.likes}</strong>
                    </span>
                    <span className="tweet-retweets">
                        <FontIcon className="material-icons" style={tweetFontIconStyles}>cached</FontIcon> <strong>{this.props.retweets}</strong>
                    </span>
                </div>
            </div>
        )
    }

}
