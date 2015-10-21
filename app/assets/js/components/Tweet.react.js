import {Card, Row, Col} from 'elemental';
import React from 'react';

const Tweet = (props) => {
    let tweet = props.tweet;
    return (
        <Card key={props.key}>
            <Row>
                <Col sm="16%"><img src={tweet.avatar} alt={tweet.username + "s avatar"} width="50" height="50"/></Col>
                <Col sm="84%">
                    <strong>@{tweet.screenname}</strong> <em>{tweet.username}</em> <br/>{tweet.text}
                </Col>
            </Row>
        </Card>
    )
};

module.exports = Tweet;
