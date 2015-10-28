import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';
import Tweet from './Tweet.react';
import UserRecommendation from './UserRecommendation.react';
import SearchBar from './SearchBar.react';
import {List, ListDivider, Paper, Card, CardText} from 'material-ui';
import config from '../util/config';
import constants from '../util/constants';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            tweets: [],
            queryResults: [],
            currentQuery: '',
            activeQuery: '',
            querySocket: null
        }
    }

    componentWillMount() {
        let tweets = new WebSocket('ws://localhost:9000/ws/default:primary');
        tweets.onmessage = event => {
            this.setState({tweets: JSON.parse(event.data)})
        };
        setInterval(() => {
            if (this.state.activeQuery !== '' && this.state.querySocket) {
                this.state.querySocket.send(JSON.stringify({
                    "channel": this.state.activeQuery,
                    "request": constants.requests.KEEP_ALIVE
                }))
            }
        }, config.keepAliveFrequency)
    }

    handleChange = (e) => {
        let raw = e.target.value;
        this.setState({currentQuery: raw})
    }

    handleEnter = (e) => {
        if (e.key === 'Enter') {
            let raw = this.state.currentQuery;

            // Get the query text without the hashtag
            let query = raw.startsWith('#') ? raw.substring(1) : raw;

            // Create the WebSocket
            let querySocket = new WebSocket(`ws://localhost:9000/ws/${query}`);
            querySocket.onmessage = event => {
                this.setState({queryResults: JSON.parse(event.data)})
            };
            if (this.state.querySocket) {
                this.state.querySocket.close();
            }
            this.setState({ querySocket: querySocket, activeQuery: this.state.currentQuery, currentQuery: '' })
        }
    }

    render() {
        let tweetList = [];
        this.state.tweets.forEach((tweet, idx) => {
            tweetList.push(<Tweet key={`tweet:${idx}`} tweet={tweet}/>);
            if (idx !== this.state.tweets.length -1) {
                tweetList.push(<ListDivider key={`divider:${idx}`} />)
            }
        });
        let queryResults = [];
        this.state.queryResults.forEach((result, idx) => {
            queryResults.push(<UserRecommendation key={`recommendation:${idx}`} username={result.username} rating={result.rating} query={result.query} />)
            if (idx !== this.state.queryResults.length -1) {
                queryResults.push(<ListDivider key={`divider:${idx}`}/>)
            }
        }, this);
        return (
            <Container maxWidth={800}>
                <Row>
                    <Col sm="100%">
                        <h1>Who To Follow In Context</h1>
                    </Col>
                </Row>
                <Row>
                    <Col sm="100%">
                        <SearchBar placeholder="Type a #hashtag to get recommendations..."
                                   buttonText="Go!"
                                   handleChange={this.handleChange}
                                   handleEnter={this.handleEnter}
                                   currentQuery={this.state.currentQuery} />
                    </Col>
                </Row>
                <Row>
                    <Col sm="50%">
                        <h3>Tweet stream</h3>
                        <p>Showing batches of 20 tweets.</p>
                        <Paper zDepth={1} rounded={false}>
                            <List>{tweetList}</List>
                        </Paper>
                    </Col>
                    <Col sm="50%">
                        <h3>Recommendations {this.state.activeQuery != '' ? 'for #' + this.state.activeQuery : ''}</h3>
                        <p>Currently based solely on hashtag counts</p>
                        <Paper zDepth={1} rounded={false}>
                            {queryResults.length > 0 ?
                                <List>{queryResults}</List> :
                                <Card><CardText>Please enter a query to see recommendations.</CardText></Card>}
                        </Paper>
                    </Col>
                </Row>
            </Container>
        )
    }

}

ReactDOM.render(
    <App />,
    document.getElementById('wtfc-app-mount')
);