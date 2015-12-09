///<reference path='../models/Twitter.ts'/>
///<reference path='../models/Learning.ts'/>

import * as React from 'react';
import * as $ from 'jquery';
import {Container, Row, Col} from 'elemental';
import {Avatar, Paper, RaisedButton, FlatButton, List, ListItem, ListDivider, Snackbar} from 'material-ui'
import Hashtag from './Hashtag.tsx';
import Configuration from "../util/config";
import TimelineApi from '../endpoints/TimelineApi';
import LearningApi from '../endpoints/LearningApi';


interface IUserInfoProps {
    params: any;
}

interface IUserInfoState {
    timeline: Array<Twitter.Status>
}

export default class UserInfo extends React.Component<IUserInfoProps, IUserInfoState> {

    constructor(props: IUserInfoProps) {
        super(props);
        this.state = {
            timeline: []
        }
    }

    componentWillMount() {
        let timelineXhr: JQueryXHR = TimelineApi.fetchAndAnalyse(this.props.params.screenName);
        timelineXhr.then(
            (results: any) => {
                let recentTweets: Array<Twitter.Status> = results.tweets;
                this.setState({
                    timeline: recentTweets
                })
            },
            (failResponse: any) => {
                console.log("An error occurred fetching the user timeline." + failResponse);
            }
        )
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

    render() {
        return (
            <Container maxWidth={940}>
                <Row>
                    <Col sm="70%">
                        <h1>@{this.props.params.screenName}</h1>
                    </Col>
                    <Col sm="30%" className="view-on-twitter-button-flex">
                        <FlatButton label="View On Twitter" onTouchTap={this._openUserInTwitter} />
                    </Col>
                </Row>
                <Row>
                    <Col sm="25%">
                        <Row>
                            <Col sm="100%">
                                <Avatar src={`http://avatars.io/twitter/${this.props.params.screenName}`} size={130}/>
                            </Col>
                        </Row>
                        <Row>
                            <Col sm="100%">
                                <h2>Features</h2>
                                <p>List of features here.</p>
                            </Col>
                        </Row>
                    </Col>


                    <Col sm="50%">
                        <h2>Timeline</h2>
                        <List>
                            {this.state.timeline.map((status: Twitter.Status) => {
                                return (
                                    <ListItem key={status.id} primaryText={status.username}
                                    secondaryText={status.text} secondaryTextLines={2} leftAvatar={<Avatar src={status.avatar} />} />
                                    );
                                })}
                        </List>
                    </Col>


                    <Col sm="25%">
                        <Row>
                            <Col sm="100%">
                                <h2>Classify User</h2>
                                <p className="">
                                Is this user tweeting high quality information
                                relating to the query topic <Hashtag hashtag={this.props.params.query} />?
                                </p>
                                <div className="classify-buttons-flex">
                                    <RaisedButton label="Yes" secondary={true} onTouchTap={this._classifyUser.bind(this, 1)} />
                                    <RaisedButton label="No" primary={true} onTouchTap={this._classifyUser.bind(this, 0)} />
                                </div>
                                <Snackbar ref="snackbar"
                                          message="Thanks for classifying this user."
                                          action="hide"
                                          autoHideDuration={3000}
                                />
                            </Col>
                        </Row>
                    </Col>

                </Row>
            </Container>
        )
    }

}