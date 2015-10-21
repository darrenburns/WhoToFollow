import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col, Card} from 'elemental';

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            htCounts: []
        }
    }

    componentWillMount() {
        let htCounts = new WebSocket('ws://localhost:9000/ws/twitter-ht-counts');
        htCounts.onmessage = event => {
            this.setState({htCounts: JSON.parse(event.data).data})
        }
    }

    render() {
        let htCountList = this.state.htCounts.map((item, idx) => {
            return (
                <Card key={idx}><em>{item.word}</em>: {item.count} occurrences</Card>
            )
        });
        return (
            <Container maxWidth={800}>
                <Row>
                    <h1>Who To Follow In Context</h1>
                </Row>
                <Row>
                    <Col sm="60%">
                        <Card>Tweet stream</Card>
                    </Col>
                    <Col sm="40%">
                        {htCountList}
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