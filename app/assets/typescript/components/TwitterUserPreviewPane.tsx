/// <reference path="../models/Twitter.ts" />


import * as React from 'react';
import * as $ from 'jquery';
import * as Immutable from 'immutable';
import * as moment from 'moment';
import {Container, Row, Col} from 'elemental';
import {Avatar, Paper, RaisedButton, FlatButton, List, ListItem, ListDivider, Snackbar, Slider, Checkbox} from 'material-ui';
import Hashtag from './Hashtag';
import Tweet from './Tweet'
import Configuration from "../util/config";
import Constants from "../util/constants";
import TimelineApi from '../endpoints/TimelineApi';
import LearningApi from '../endpoints/LearningApi';
import Status = Twitter.Status;


interface ITwitterUserPreviewPaneProps {
    params: any;
}

interface ITwitterUserPreviewPaneState {
    timeline?: Immutable.List<Twitter.Status>;
    name?: string;
    coverPhotoUrl?: string;
    avatarUrl?: string;
    profileColour?: string;
    userSocket?: WebSocket;
    latestFeaturesUpdate?: Immutable.Map<string, number>;
    latestMedianTimeSinceHashtag?: moment.Moment;
    socketKeepAliveHandle?: number;
}

export default class TwitterUserPreviewPane extends
    React.Component<ITwitterUserPreviewPaneProps, ITwitterUserPreviewPaneState> {

    constructor(props: ITwitterUserPreviewPaneProps) {
        super(props);
        this.state = {
            timeline: Immutable.List<Twitter.Status>(),
            latestFeaturesUpdate: Immutable.Map<string, number>()
        }
    }

    componentDidMount() {
        this._setUserTimeline(this.props.params.screenName);
        // Listen to this users channel
        this._setUserChannel(this.props.params.screenName);
        // Set the correct default checkbox value
        let checkbox: any = this.refs['relevance-checkbox'];
        LearningApi.getUserRelevance(this.props.params.screenName)
            .then(
                doneResponse => {
                    checkbox.setChecked(doneResponse.isChecked);
                },
                failResponse => {
                    console.log("Error fetching user relevance status.")
                }
            )
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

    private _markUserRelevance = (event: any): void => {
        let checkbox: any = this.refs['relevance-checkbox'];
        LearningApi.markUserAsRelevant(this.props.params.screenName, this.props.params.query, checkbox.isChecked());
    };

    private _setUserChannel = (screenName: string): void => {
        let ws: WebSocket = new WebSocket(`ws://localhost:9000/ws/user:${screenName}`);
        ws.onmessage = (event) => {
            let update: Learning.UserFeatures = JSON.parse(event.data);
            let newFeatures = Immutable.Map<string, number>();
            // Store the latest features in component state for display
            for (let key of Object.keys(update)) {
                if (key !== 'hashtagTimestamps') {
                    newFeatures = newFeatures.set(key, update[key]);
                }
            }
            // Filter out hashtags which aren't the same as the query.
            let allTimestamps: Array<Array<any>> = update['hashtagTimestamps'];  // Array of [hashtag, timestamp]
            let relevantTimestamps = allTimestamps.filter(elem => {
                let hashtag: string = elem['hashtag'];
                return hashtag.toLowerCase() === this.props.params.query.toLowerCase();
            });
            // Get the latest median time since mention of the hashtag
            let median = 0;
            let mid = Math.floor((relevantTimestamps.length - 1)/ 2);
            if (relevantTimestamps.length < 1) {
                median = null;
            } else {
                if (relevantTimestamps.length % 2 === 0) {
                    median = (relevantTimestamps[mid]['timestamp'] + relevantTimestamps[mid+1]['timestamp'])/2;
                } else {
                    median = relevantTimestamps[mid]['timestamp'];
                }
            }
            this.setState({
                latestFeaturesUpdate: newFeatures,
                latestMedianTimeSinceHashtag: median === null ? null : moment(median * 1000)  // Convert to milliseconds for momentjs
            });
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
                let recentTweets: Immutable.List<Twitter.Status> = results.timeline;
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
        // Close socket for this user and prevent this client from sending more keep-alives.
        let sock: WebSocket = this.state.userSocket;
        if (sock != null) {
            sock.close();
        }
        let kah: number = this.state.socketKeepAliveHandle;
        if (kah != null) {
            clearInterval(kah);
        }
        // Clear information to prevent confusion in the time before information for the new user is fetched.
        this.setState({
            latestFeaturesUpdate: Immutable.Map<string, number>(),
            timeline: Immutable.List<Status>(),
            latestMedianTimeSinceHashtag: null,
            name: ""
        })
    }

    render() {
        let tweetsProcessed = this.state.latestFeaturesUpdate.get('tweetCount', null);
        let followersCount = this.state.latestFeaturesUpdate.get('followerCount', null);
        let wordsCounted = this.state.latestFeaturesUpdate.get('wordCount', null);
        let capitalCount = this.state.latestFeaturesUpdate.get('capitalisedCount', null);
        let hashtagCount = this.state.latestFeaturesUpdate.get('hashtagCount', null);
        let retweetCount = this.state.latestFeaturesUpdate.get('retweetCount', null);
        let likeCount = this.state.latestFeaturesUpdate.get('likeCount', null);
        let dictionaryHits = this.state.latestFeaturesUpdate.get('dictionaryHits', null);
        let linkCount = this.state.latestFeaturesUpdate.get('linkCount', null);

        let coverStyles = {
            backgroundImage: this.state.coverPhotoUrl !== "unknown" ? `url(${this.state.coverPhotoUrl})` : null,
            backgroundColor: `#${this.state.profileColour}`  // Will be shown when there is no profile picture
        };

        let profileTrimStyle = {
            borderTop: `3px solid #${this.state.profileColour}`
        };

        let latestMedianText = (
                <span className="user-preview-feature-item">
                    {this.state.latestMedianTimeSinceHashtag === null ?
                        "User has never used the hashtag #" + this.props.params.query :
                        <span><strong>{(moment().diff(this.state.latestMedianTimeSinceHashtag, 'seconds')/60).toFixed(1)}</strong> median minutes since use of #{this.props.params.query}</span>
                        }
                </span>
        );


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
                    <div className="user-preview-features" style={profileTrimStyle}>
                        <span className="user-preview-feature-item"><strong>{followersCount}</strong> followers</span>
                        <span className="user-preview-feature-item"><strong>{tweetsProcessed}</strong> tweets processed</span>
                        <span className="user-preview-feature-item"><strong>{wordsCounted}</strong> words</span>
                        <span className="user-preview-feature-item"><strong>{hashtagCount}</strong> hashtags</span>
                        <span className="user-preview-feature-item"><strong>{capitalCount && wordsCounted ? (100*capitalCount/wordsCounted).toFixed(1) + '%' : null}</strong> capitalised words</span>
                        <span className="user-preview-feature-item"><strong>{dictionaryHits && wordsCounted ? (100*dictionaryHits/wordsCounted).toFixed(1) + '%' : null}</strong> spelling accuracy</span>
                        <span className="user-preview-feature-item"><strong>{likeCount}</strong> likes from others</span>
                        <span className="user-preview-feature-item"><strong>{retweetCount}</strong> retweets from others</span>
                        {latestMedianText}
                    </div>

                    <div className="user-preview-body">
                        <div className="user-preview-body-header">
                            <h3>Most Recent Tweets</h3>
                            <div className="relevance-checkbox-wrapper">
                                <Checkbox
                                    label="Relevant?"
                                    ref="relevance-checkbox"
                                    onCheck={this._markUserRelevance}
                                />
                            </div>
                        </div>
                        <div className="tweet-list">
                            {
                                this.state.timeline.map((status: Twitter.Status) =>
                                    <Tweet key={status.id} status={status} query={this.props.params.query}
                                           colour={this.state.profileColour === "FFFFFF" ? "0b97c2" : this.state.profileColour} />)
                            }
                        </div>
                    </div>
            </div>
        )
    }

}