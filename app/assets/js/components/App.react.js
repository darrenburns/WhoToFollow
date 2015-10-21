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
            hashtagCounts: []
        }
    }

    componentWillMount() {
        let tweets = new WebSocket('ws://localhost:9000/ws/default:primary');
        let hashtagCounts = new WebSocket('ws://localhost:9000/ws/test');
        tweets.onmessage = event => {
            this.setState({tweets: JSON.parse(event.data)})
        };
        hashtagCounts.onmessage = event => {
            this.setState({hashtagCounts: JSON.parse(event.data)})
        };
    }

    render() {
        let tweetList = this.state.tweets.map((tweet, idx) => {
            return (
                <Tweet key={idx} tweet={tweet}/>
            )
        });
        let htCounts = this.state.hashtagCounts.map((item, idx) => {
            return (
                <Card key={idx}><strong>{item.word}</strong>: {item.count} occurrences</Card>
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
                            buttonText="Go!" />
                    </Col>
                </Row>
                <Row>
                    <Col sm="50%">
                        <h3>Tweet stream</h3>
                        <p>Showing batches of 20 tweets.</p>
                        {tweetList}
                    </Col>
                    <Col sm="50%">
                        <h3>Word counts</h3>
                        <p>Over a 10 minutes window. Updates every 2 seconds.</p>
                        {htCounts}
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