import * as React from 'react';
import {History} from 'react-router';
import UserRecommendation from './UserRecommendation.tsx';
import Hashtag from './Hashtag.tsx';
import {Row, Col} from 'elemental';
import {GridList, IconButton, CircularProgress, Paper, FlatButton} from 'material-ui';
import * as Immutable from 'immutable';
import {velocityHelpers, VelocityComponent, VelocityTransitionGroup} from 'velocity-react';
import Configuration from '../util/config';
import Constants from '../util/constants';
import List = Immutable.List;
import Map = Immutable.Map;


interface UserScore {
    screenName: string,
    name: string,
    score: number
}

const QueryResults = React.createClass({

    mixins: [History],

    getInitialState: function() {
        return {
            querySocket: null,
            queryResults: Immutable.List([]),
            queryComplete: false,
            keepAlive: null,
            queryUserHistories: Immutable.Map({})
        }
    },

    componentDidMount: function() {
        this._setQueryChannel(this.props.params.query);
    },

    componentWillUnmount: function() {
        this._freeComponentResources();
    },

    componentDidUpdate: function(prevProps: any): void {
        if (prevProps.params['query'] !== this.props['params']['query']) {
            this._freeComponentResources();
            // Clear the current query information to prevent confusion during load
            this.setState({
                queryResults: Immutable.List(),
                queryUserHistories: Immutable.Map(),
                queryComplete: false
            });
            this._setQueryChannel(this.props.params.query);
        }
    },

    _setQueryChannel(query: string): void {
        let querySocket = new WebSocket(`ws://localhost:9000/ws/${this.props.params.query}`);
        querySocket.onmessage = event => {
            let recs: Array<UserScore> = JSON.parse(event.data).results;
            let history: Map<string, List<number>> = this.state.queryUserHistories;
            recs.forEach((rec: UserScore) => {
                let screenName: string = rec.screenName;
                if (history.has(screenName)) {
                    let userData: List<number> = history.get(screenName);
                    history = history.set(screenName, userData.push(rec.score));
                } else {
                    history = history.set(screenName, Immutable.List([]));
                }
            });
            this.setState({
                queryComplete: true,
                queryResults: Immutable.List(recs),
                queryUserHistories: history
            });
        };
        // Continuously send Keep-Alives to inform server that we still want recs and stats
        let keepAliveTrigger = setInterval(() => {
            console.log(`Sending keep alive for query channel ${query}`);
            if (this.props.params.query !== '' && this.state.querySocket) {
                this.state.querySocket.send(JSON.stringify({
                    "channel": this.props.params.query,
                    "request": Constants.KEEP_ALIVE_STRING
                }))
            }
        }, Configuration.KEEP_ALIVE_FREQUENCY);
        this.setState({
            querySocket: querySocket,
            keepAlive: keepAliveTrigger
        });
    },

    _freeComponentResources(): void {
        if (this.state.querySocket != null) {
            this.state.querySocket.close();
        }
        if (this.state.keepAlive != null) {
            clearInterval(this.state.keepAlive);
        }
    },

    render: function() {

        /*
        Animation related stuff
         */
        var enterAnimation = {
            animation: "fadeIn",
            backwards: true
        };

        var leaveAnimation = {
            animation: "fadeOut",
            duration: 1500,
            backwards: true
        };

        let queryResults = this.state.queryResults.map((result: UserScore) => {
            let hist: List<number> = this.state.queryUserHistories.get(result.screenName);
            return (
                <UserRecommendation key={result.screenName}
                                    query={this.props.params.query}
                                    name={result.name}
                                    screenName={result.screenName}
                                    score={result.score}
                                    userHistory={hist}
                />
            );
        }, this);
        let spinner = <CircularProgress mode="indeterminate" />;
        let queryResultMessage = <h2 className="padded-top-header">Terrier suggests <strong>{queryResults.size}</strong> users for '<span className="query-text">{this.props.params.query}</span>'.</h2>;
        if (this.state.queryComplete && queryResults.length === 0) {
            queryResultMessage = <h2 className="padded-top-header">There were no results for the query '<span className="query-text">{this.props.params.query}</span>'.</h2>;
        }
        return (
            <Row>
                <Col sm="100%">
                    <Row>
                        <Col sm="100%">
                            {queryResultMessage}
                        </Col>
                    </Row>
                    <Row>
                        <Col sm="45%">
                            {queryResults.size > 0 ?
                                <div className="recommendations">
                                    {queryResults}
                                </div>
                                :
                                (!this.state.queryComplete ?
                                    spinner :
                                    null)}

                        </Col>
                        <Col sm="55%">
                            {/* The TwitterUserPreview pane will be loaded in here */}
                            {this.props.children}
                            {  /* Display instructions if results have been returned */
                                queryResults.size > 0 && this.state.queryComplete ?
                                    <div className="rec-list-instruction-pane">
                                        <p>
                                            To view a user's profile, select their
                                            screen name from the list of recommendations
                                            on the left.
                                        </p>
                                    </div>
                                    :
                                    null
                                }
                        </Col>
                    </Row>
                </Col>
            </Row>
        )
    }

});

export default QueryResults;