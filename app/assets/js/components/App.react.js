import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col, Card, InputGroup,
    FormInput, Button, Spinner} from 'elemental';

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
        let tweetList = this.state.tweets.map((item, idx) => {
            return (
                <Card key={idx}>
                    <Row>
                        <Col sm="20%"><img src={item.avatar} alt={item.username + "s avatar"} width="50" height="50"/></Col>
                        <Col sm="80%">
                            <strong>@{item.screenname}</strong> <em>{item.username}</em> <br/>{item.text}
                        </Col>
                    </Row>
                </Card>
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
                        <InputGroup contiguous>
                            <InputGroup.Section grow>
                                <FormInput type="text" placeholder="Type a #hashtag to get recommendations..." />
                            </InputGroup.Section>
                            <InputGroup.Section>
                                <Button type="primary">Go!</Button>
                            </InputGroup.Section>
                        </InputGroup>
                    </Col>
                </Row>
                <Row>
                    <Col sm="50%">
                        <h3>Tweet stream</h3>
                        <p>Showing batches of 20 tweets.</p>
                        <hr/>
                        {tweetList}
                    </Col>
                    <Col sm="50%">
                        <h3>Word counts</h3>
                        <p>Over a 10 minutes window. Updates every 2 seconds.</p>
                        <hr/>
                        {htCounts || <Spinner size="lg" />}
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