import {Row, Col} from 'elemental';
import {ListItem, Avatar} from 'material-ui';
import * as React from 'react';

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

export default Tweet;
