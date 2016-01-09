/// <reference path="../models/Twitter.ts" />


import * as React from 'react';
import * as $ from 'jquery';
import * as Immutable from 'immutable';
import {Container, Row, Col} from 'elemental';
import {Avatar, Paper, RaisedButton, FlatButton, List, ListItem, ListDivider, Snackbar} from 'material-ui'
import Hashtag from './Hashtag.tsx';
import Configuration from "../util/config";
import Constants from "../util/constants";
import TimelineApi from '../endpoints/TimelineApi';
import LearningApi from '../endpoints/LearningApi';


interface ITwitterUserPreviewPaneProps {
    params: any;
}

interface ITwitterUserPreviewPaneState {
    timeline?: Array<Twitter.Status>;
    name?: string;
    coverPhotoUrl?: string;
    avatarUrl?: string;
    profileColour?: string;
    userSocket?: WebSocket;
    latestFeaturesUpdate?: Immutable.Map<string, number>;
    socketKeepAliveHandle?: number;
}

export default class TwitterUserPreviewPane extends
    React.Component<ITwitterUserPreviewPaneProps, ITwitterUserPreviewPaneState> {

    constructor(props: ITwitterUserPreviewPaneProps) {
        super(props);
        this.state = {
            timeline: [],
            latestFeaturesUpdate: Immutable.Map<string, number>()
        }
    }

    componentDidMount() {
        this._setUserTimeline(this.props.params.screenName);
        // Listen to this users channel
        this._setUserChannel(this.props.params.screenName);
    }

    componentDidUpdate(prevProps: ITwitterUserPreviewPaneProps) {
        let newScreenName = this.props.params.screenName;
        if (prevProps.params.screenName !== newScreenName) {
            this._freeComponentResources();
            this._setUserTimeline(newScreenName);
            this._setUserChannel(newScreenName);
        }
    }

    componentWillUnmount() {
        this._freeComponentResources();
    }

    private _openUserInTwitter = (): void => {
        window.open(`https://twitter.com/${this.props.params.screenName}`);
    };

    private _classifyUser = (clazz: number): void => {
        console.log(clazz);
        LearningApi.classifyUser(this.props.params.screenName, this.props.params.query, clazz);
        let snackbar: any = this.refs['snackbar'];
        snackbar.show();
    };

    private _setUserChannel = (screenName: string): void => {
        let ws:WebSocket = new WebSocket(`ws://localhost:9000/ws/user:${screenName}`);
        ws.onmessage = (event) => {
            let update: Learning.UserFeatures = JSON.parse(event.data);
            let newFeatures = Immutable.Map<string, number>();
            for (let key of Object.keys(update)) {
                newFeatures = newFeatures.set(key, update[key]);
            }
            this.setState({latestFeaturesUpdate: newFeatures});
        };
        if (this.state.userSocket != null) {
            this.state.userSocket.close();
        }
        let keepAliveHandle = setInterval(() => {
            if (this.props.params.screenName !== '') {
                this.state.userSocket.send(JSON.stringify({
                    "channel": `user:${this.props.params.screenName}`,
                    "request": Constants.KEEP_ALIVE_STRING
                }))
            }
        }, Configuration.KEEP_ALIVE_FREQUENCY);
        this.setState({socketKeepAliveHandle: keepAliveHandle, userSocket: ws});
    };

    private _setUserTimeline = (screenName: string): void => {
        let timelineXhr:JQueryXHR = TimelineApi.fetchAndAnalyse(screenName);
        timelineXhr.then(
            (results:any) => {
                let recentTweets: Array<Twitter.Status> = results.timeline;
                this.setState({
                    avatarUrl: results.metadata.avatarUrl,
                    name: results.metadata.name,
                    coverPhotoUrl: results.metadata.coverPhotoUrl,
                    profileColour: results.metadata.profileColour,
                    timeline: recentTweets
                })
            },
            (failResponse:any) => {
                console.log("An error occurred fetching the user timeline." + failResponse);
            }
        );

    };

    private _freeComponentResources(): void {
        let sock: WebSocket = this.state.userSocket;
        if (sock != null) {
            sock.close();
        }
        let kah: number = this.state.socketKeepAliveHandle;
        if (kah != null) {
            clearInterval(kah);
        }
    }

    render() {
        console.log("Features size: " + this.state.latestFeaturesUpdate.size);
        let tweetsProcessed = this.state.latestFeaturesUpdate.get('tweetCount', 0);
        console.log("In render - Got tweets processed: " + tweetsProcessed);
        let followersCount = this.state.latestFeaturesUpdate.get('followerCount', 0);
        let wordsCounted = this.state.latestFeaturesUpdate.get('wordCount', 0);
        let capitalCount = this.state.latestFeaturesUpdate.get('capitalisedCount', 0);
        let hashtagCount = this.state.latestFeaturesUpdate.get('hashtagCount', 0);
        let retweetCount = this.state.latestFeaturesUpdate.get('retweetCount', 0);
        let likeCount = this.state.latestFeaturesUpdate.get('likeCount', 0);
        let dictionaryHits = this.state.latestFeaturesUpdate.get('dictionaryHits', 0);
        let linkCount = this.state.latestFeaturesUpdate.get('linkCount', 0);

        let coverStyles = {
            backgroundImage: this.state.coverPhotoUrl !== "unknown" ? `url(${this.state.coverPhotoUrl})` : null,
            backgroundColor: `#${this.state.profileColour}`
        };
        return (

            <div className="user-preview-pane">
                <div className="user-cover-photo" style={coverStyles}>
                    <img className="user-profile-image"
                            height="100px" width="100px"
                           src={this.state.avatarUrl}
                           alt={this.props.params.screenName}
                    />
                    <div className="user-cover-names">
                        <span className="user-profile-name">{this.state.name}</span>
                        <br/>
                        <span className="user-profile-screenname">@{this.props.params.screenName}</span>
                    </div>
                </div>
                <div className="user-preview-features">
                    <span className="user-preview-feature-item"><strong>{followersCount}</strong> followers</span>
                    <span className="user-preview-feature-item"><strong>{tweetsProcessed}</strong> tweets processed</span>
                    <span className="user-preview-feature-item"><strong>{wordsCounted}</strong> words</span>
                    <span className="user-preview-feature-item"><strong>{hashtagCount}</strong> hashtags</span>
                    <span className="user-preview-feature-item"><strong>{(capitalCount/wordsCounted).toFixed(1)}%</strong> capitalised words</span>
                    <span className="user-preview-feature-item"><strong>{(dictionaryHits/wordsCounted).toFixed(1)}%</strong> spelling accuracy</span>
                    <span className="user-preview-feature-item"><strong>{likeCount}</strong> likes from others</span>
                    <span className="user-preview-feature-item"><strong>{retweetCount}</strong> retweets from others</span>
                </div>
                <div className="user-preview-body">
                    <h2>Most Recent Tweets</h2>
                    <p>List of tweets here</p>
                </div>
            </div>

        )
    }

}
// OLD LAYOUT
//<Container maxWidth={940}>
//    <Row>
//        <Col sm="70%">
//            <h1>@{this.props.params.screenName}</h1>
//        </Col>
//        <Col sm="30%" className="view-on-twitter-button-flex">
//            <FlatButton label="View On Twitter" onTouchTap={this._openUserInTwitter} />
//        </Col>
//    </Row>
//    <Row>
//        <Col sm="25%">
//            <Row>
//                <Col sm="100%">
//                    <Avatar src={`http://avatars.io/twitter/${this.props.params.screenName}`} size={130}/>
//                </Col>
//            </Row>
//            <Row>
//                <Col sm="100%">
//                    <h2>Features</h2>
//                    <ul>
//                        <li><strong>Tweets Processed</strong>: {tweetsProcessed}</li>
//                        <li><strong>Followers</strong>: {followersCount}</li>
//                        <li><strong>Words Counted</strong>: {wordsCounted}</li>
//                        <li><strong>% Capitalised Words</strong>: {(100*capitalCount/wordsCounted).toFixed(2)}%</li>
//                        <li><strong>Hashtags Encountered</strong>: {hashtagCount}</li>
//                        <li><strong>Times Retweeted</strong>: {retweetCount}</li>
//                        <li><strong>Times Liked</strong>: {likeCount}</li>
//                        <li><strong>Links Used</strong>: {linkCount}</li>
//                        <li><strong>Spelling Accuracy</strong>:
//                                    {(100*dictionaryHits/wordsCounted).toFixed(2)}%</li>
//                    </ul>
//                </Col>
//            </Row>
//        </Col>
//
//
//        <Col sm="50%">
//            <h2>Timeline</h2>
//            <List>
//                {this.state.timeline.map((status: Twitter.Status) => {
//                    return (
//                    <ListItem key={status.id} primaryText={status.username}
//                              secondaryText={status.text} secondaryTextLines={2} leftAvatar={<Avatar src={status.avatar} />} />
//                        );
//                    })}
//            </List>
//        </Col>
//
//
//        <Col sm="25%">
//            <Row>
//                <Col sm="100%">
//                    <h2>Classify User</h2>
//                    <p className="">
//                        Is this user tweeting high quality information
//                        relating to the query topic <Hashtag hashtag={this.props.params.query} />?
//                    </p>
//                    <div className="classify-buttons-flex">
//                        <RaisedButton label="Yes" secondary={true} onTouchTap={this._classifyUser.bind(this, 1)} />
//                        <RaisedButton label="No" primary={true} onTouchTap={this._classifyUser.bind(this, 0)} />
//                    </div>
//                    <Snackbar ref="snackbar"
//                              message="Thanks for classifying this user."
//                              action="hide"
//                              autoHideDuration={3000}
//                    />
//                </Col>
//            </Row>
//        </Col>
//
//    </Row>
//</Container>