import React from 'react';
import ReactDOM from 'react-dom';
import {Container, Row, Col} from 'elemental';

class App extends React.Component {

    render() {
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
                        <Card>Recommended</Card>
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