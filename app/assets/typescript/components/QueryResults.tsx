///<reference path='../node_modules/immutable/dist/Immutable.d.ts'/>

import * as React from 'react';
import {History} from 'react-router';
import UserRecommendation from './UserRecommendation.tsx';
import Hashtag from './Hashtag.tsx';
import {Row, Col} from 'elemental';
import {GridList, IconButton, CircularProgress, Paper, FlatButton} from 'material-ui';
import * as Immutable from 'immutable';
import Configuration from '../util/config';
import Constants from '../util/constants';
import List = Immutable.List;
import Map = Immutable.Map;

interface UserScore {
    screenName: string,
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
        let queryResults = [];
        this.state.queryResults.forEach((result: UserScore, idx: number) => {
            let hist: List<number> = this.state.queryUserHistories.get(result.screenName);
            queryResults.push(
                <UserRecommendation key={`recommendation:${idx}`}
                                    screenName={result.screenName}
                                    score={result.score + 1}
                                    userHistory={hist}
                />
            );
        }, this);
        let spinner = <CircularProgress mode="indeterminate" />;
        let noResultsMessage = <Paper zDepth={1}><p>Your query returned no results.</p></Paper>;
        return (
            <Row>
                <Col sm="100%">
                    <Row>
                        <Col sm="100%">
                            <h2>{queryResults.length} Results for <Hashtag hashtag={this.props.params.query}/></h2>
                        </Col>
                    </Row>
                    <Row>
                        <Col sm="100%">
                            {queryResults.length > 0 ?
                                    <GridList
                                        cols={4}
                                        padding={3}
                                        cellHeight={200}>
                                        {queryResults}
                                    </GridList>
                                :
                                (this.state.queryComplete ?
                                    noResultsMessage :
                                    spinner)}

                        </Col>
                    </Row>
                </Col>
            </Row>
        )
    }

});

export default QueryResults;