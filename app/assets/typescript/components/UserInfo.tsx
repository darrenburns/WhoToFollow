import * as React from 'react';
import {Container, Row, Col} from 'elemental';
import {Avatar, Paper, RaisedButton} from 'material-ui'
import Hashtag from './Hashtag.tsx';


interface UserInfoProps {
    screenName: string;
    params: any;
}

export default class UserInfo extends React.Component<UserInfoProps, any> {

    constructor(props) {
        super(props);
    }

    componentWillMount() {
        // Make Ajax request to the backend to get the user's timeline

    }

    render() {
        return (
            <Container maxWidth={800}>
                <Row>
                    <Col sm="100%">
                        <h1>@{this.props.params.screenName}</h1>
                    </Col>
                </Row>
                <Row>
                    <Col sm="25%">
                        <Row>
                            <Col sm="100%">
                                <Avatar src={`http://avatars.io/twitter/${this.props.params.screenName}`} size={130}/>
                            </Col>
                        </Row>
                        <Row>
                            <Col sm="100%">
                                <h2>Features</h2>
                                <p>List of features here.</p>
                            </Col>
                        </Row>
                    </Col>


                    <Col sm="45%">
                        <h2>Timeline</h2>
                        List of the user's tweets here
                    </Col>


                    <Col sm="30%">
                        <Row>
                            <Col sm="100%">
                                <h2>Classify User</h2>
                                <p className="">
                                Is this user tweeting high quality information
                                relating the query topic <Hashtag hashtag={this.props.params.query} />?
                                </p>
                                <div className="classify-buttons-flex">
                                    <RaisedButton label="Yes" secondary={true}/>
                                    <RaisedButton label="No" primary={true}/>
                                </div>
                            </Col>
                        </Row>
                    </Col>

                </Row>
            </Container>
        )
    }

}