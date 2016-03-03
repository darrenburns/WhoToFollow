import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as Immutable from 'immutable';
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
import {RecentQuery} from "./App";


interface HomeState {
    currentQuery: string
}

interface HomeProps {
    recentQueries: Immutable.List<RecentQuery>
}

const Home = React.createClass<HomeProps, HomeState>({

    mixins: [History],

    getInitialState() {
        return {
            currentQuery: '',
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
                        <Row>
                            <Col sm="100%">
                            <p>Enter a hashtag into the box below to begin. If users are discussing that hashtag you
                            will see a list of suggested users. By selecting these users, you can indicate that they
                            are relevant given your query by selecting "Mark As Relevant" on the right hand side of
                            their profile. This will train the application to improve the relevance of future results!</p>
                            </Col>
                        </Row>
                        <Row>
                            <Col sm="100%">
                            <h3 className="padded-top-header">Search</h3>
                            <SearchBar placeholder="Type a hashtag to get recommendations..."
                                       handleChange={this.handleChange}
                                       handleEnter={this.handleEnter}
                                       currentQuery={this.state.currentQuery} />
                            </Col>
                        </Row>
                    </Col>
                    <Col lg="50%">
                        <h3 className="padded-top-header">Recent Searches</h3>
                        <ScrollingList duration={500} numItemsToShow={4} items={this.props.recentQueries} />
                    </Col>
                </Row>
                {this.props.children}
            </div>
        )
    }

});
export default Home;