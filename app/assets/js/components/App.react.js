import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col, Card} from 'elemental';
import Tweet from './Tweet.react';
import SearchBar from './SearchBar.react';

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
    }

    handleChange = (e) => {
        let raw = e.target.value;
        console.log(e);
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
        let tweetList = this.state.tweets.map((tweet, idx) => {
            return (
                <Tweet key={idx} tweet={tweet}/>
            )
        });
        let queryResults = this.state.queryResults.map((result, idx) => {
            return (
                <Card key={idx}><strong>@{result.username}</strong> - {result.rating}</Card>
            )
        });
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
                            buttonText="Go!" handleChange={this.handleChange} handleEnter={this.handleEnter} currentQuery={this.state.currentQuery} />
                    </Col>
                </Row>
                <Row>
                    <Col sm="50%">
                        <h3>Tweet stream</h3>
                        <p>Showing batches of 20 tweets.</p>
                        {tweetList}
                    </Col>
                    <Col sm="50%">
                        <h3>Recommendations {this.state.activeQuery != '' ? 'for #' + this.state.activeQuery : ''}</h3>
                        <p>Currently based solely on hashtag counts</p>
                        {queryResults}
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