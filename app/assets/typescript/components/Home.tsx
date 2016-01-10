import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import Tweet from './Tweet.tsx';
import UserRecommendation from './UserRecommendation.tsx';
import SearchBar from './SearchBar.tsx';
import {ScrollingList, ScrollingListItem} from './ScrollingList.tsx';
import Hashtag from './Hashtag.tsx';
import {List, ListDivider, Paper, Card, CardText} from 'material-ui';
import {Route, Router, Link, History} from 'react-router';
import config from '../util/config';
import constants from '../util/constants';


export interface RecentQuery {
    query: string
    id: number
    timestamp: number
}


interface HomeState {
    currentQuery: string
    recentQueries: Array<string>
    recentQueriesSocket: WebSocket
}

const Home = React.createClass<any, HomeState>({

    mixins: [History],

    getInitialState() {
        return {
            currentQuery: '',
            recentQueries: [],
            recentQueriesSocket: null
        }
    },

    componentWillMount() {
        let ws: WebSocket = new WebSocket(`ws://localhost:9000/ws/default:recent-queries`);
        ws.onmessage = event => {
            let data = JSON.parse(event.data);
            if (data.hasOwnProperty("query")) {
                this.setState({recentQueries: [data].concat(this.state.recentQueries).slice(0, 5)})
            }
        };
        if (this.state.recentQueriesSocket != null) {
            this.state.recentQueriesSocket.close();
        }
        this.setState({
            recentQueriesSocket: ws
        });
    },

    componentWillUnmount() {
        if (this.state.recentQueriesSocket != null) {
            this.state.recentQueriesSocket.close();
        }
    },

    handleChange: function(e) {
        let raw = e.target.value;
        this.setState({currentQuery: raw})
    },

    handleEnter: function(e) {
        if (e.key === 'Enter') {
            let raw = this.state.currentQuery;

            // Get the query text without the hashtag
            let query = raw.startsWith('#') ? raw.substring(1) : raw;

            // Go to the query page
            this.history.pushState(null, `/query/${query}`);
        }
    },

    render: function() {
        return (
            <div>
                <Row>
                    <Col lg="50%">
                        <h3 className="padded-top-header">Search</h3>
                        <SearchBar placeholder="Type a query to get recommendations..."
                                   handleChange={this.handleChange}
                                   handleEnter={this.handleEnter}
                                   currentQuery={this.state.currentQuery} />
                    </Col>
                    <Col lg="50%">
                        <h3 className="padded-top-header">Recent Searches</h3>
                        <ScrollingList duration={500} numItemsToShow={4} items={this.state.recentQueries} />
                    </Col>
                </Row>
                {this.props.children}
            </div>
        )
    }

});
export default Home;