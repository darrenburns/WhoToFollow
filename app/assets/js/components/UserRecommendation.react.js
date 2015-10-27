import React from 'react';
import {Styles, Avatar, ListItem, ListDivider} from 'material-ui';

let {Colors} = Styles;

class UserRecommendation extends React.Component {

    constructor(props) {
        super(props);
    }

    openUserTwitterProfile = (username) => {
        window.open(`https://twitter.com/${username}`)
    }

    render() {
        return(
            <ListItem key={this.props.key}
                      leftAvatar={<Avatar src={`http://avatars.io/twitter/${this.props.username}`} />}
                      onTouchStart={this.openUserTwitterProfile.bind(this.props.username)}
                      primaryText={`@${this.props.username}`}
                      secondaryText={
                          <p>
                            Mentioned
                            <span style={{color: Colors.darkBlack}}> #{this.props.query} </span>
                            on <span style={{color: Colors.darkBlack}}>{this.props.rating}</span> occassions.
                          </p>
                        }
                />
        )
    }

}

module.exports = UserRecommendation;