import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import Tweet from './Tweet.react';
import UserRecommendation from './UserRecommendation.react';
import SearchBar from './SearchBar.react';
import Hashtag from './Hashtag.react';
import {List, ListDivider, Paper, Card, CardText} from 'material-ui';
import {Route, Router, Link, History} from 'react-router';
import config from '../util/config';
import constants from '../util/constants';

const Home = React.createClass({

    mixins: [History],

    getInitialState(props) {
        return {
            currentQuery: ''
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
            <Container maxWidth={800}>
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
                    <Col sm="100%">
                        <h3>Recent searches</h3>
                    </Col>
                </Row>
                <Row>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                    <Col sm="1/3"><Hashtag hashtag="Hello"/></Col>
                </Row>
            </Container>
        )
    }

});
module.exports = Home;

