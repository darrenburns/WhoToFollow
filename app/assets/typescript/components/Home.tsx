import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import Tweet from './Tweet.tsx';
import UserRecommendation from './UserRecommendation.tsx';
import SearchBar from './SearchBar.tsx';
import Hashtag from './Hashtag.tsx';
import {List, ListDivider, Paper, Card, CardText} from 'material-ui';
import {Route, Router, Link, History} from 'react-router';
import config from '../util/config';
import constants from '../util/constants';


const Home = React.createClass({

    mixins: [History],

    getInitialState() {
        return {
            currentQuery: '',
            recentQueries: [],
            recentQueriesSocket: null
        }
    },

    componentWillMount() {
        let ws = new WebSocket(`ws://localhost:9000/ws/default:recent-queries`);
        ws.onmessage = event => {
            this.setState({recentQueries: JSON.parse(event.data).recentQueries});
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
        console.log("Original this.state.recentQueries", this.state.recentQueries);
        let recentHashtags = this.state.recentQueries.map((queryString, idx) => {
            console.log("Returning hashtag");
            return <Hashtag key={idx} hashtag={queryString}/>
        });
        console.log("Mapped", recentHashtags);
        return (
            <div>
                <Row>
                    <Col sm="100%">
                        <span className="splash-text">Who To Follow</span>
                    </Col>
                </Row>
                <Row>
                    <Col sm="100%">
                        <SearchBar placeholder="Type a #hashtag to get recommendations..."
                                   handleChange={this.handleChange}
                                   handleEnter={this.handleEnter}
                                   currentQuery={this.state.currentQuery} />
                    </Col>
                </Row>
                <Row>
                    <Col sm="50%">
                        <h3 className="padded-top-h">Recent searches</h3>
                        <ul>
                            {recentHashtags.map(ht => <li>{ht}</li>)}
                        </ul>
                    </Col>
                </Row>
            </div>
        )
    }

});
export default Home;