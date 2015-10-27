import {Row, Col} from 'elemental';
import {ListItem, Avatar} from 'material-ui';
import React from 'react';

const Tweet = (props) => {
    let tweet = props.tweet;



    return (
        <ListItem key={props.key}
                  leftAvatar={<Avatar src={tweet.avatar} />}
                  primaryText={tweet.username}
                  secondaryText={<p>{tweet.text}</p>}
                  secondaryTextLines={2}
            />
    )
};

            //<Row>
            //    <Col sm="16%"><img src={tweet.avatar} alt={tweet.username + "s avatar"} width="50" height="50"/></Col>
            //    <Col sm="84%">
            //        <strong>@{tweet.screenname}</strong> <em>{tweet.username}</em> <br/>{tweet.text}
            //    </Col>
            //</Row>
module.exports = Tweet;
