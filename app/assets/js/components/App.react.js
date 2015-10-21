import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col, Card} from 'elemental';

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
        let tweetList = this.state.tweets.map(item => {
            return (
                <Card key={item.id}><strong>{item.screenname}</strong> <em>@{item.username}</em> <br/>{item.text}</Card>
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
                    <h1>Who To Follow In Context</h1>
                </Row>
                <Row>
                    <Col sm="60%">
                        <h2>Tweet stream</h2>
                        {tweetList}
                    </Col>
                    <Col sm="40%">
                        <h2>Hashtag counts</h2>
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