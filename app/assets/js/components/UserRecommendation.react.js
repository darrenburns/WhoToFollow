import React from 'react';
import {Styles, Avatar, ListItem, ListDivider, IconButton, MoreVertIcon} from 'material-ui';
import injectTapEventPlugin from 'react-tap-event-plugin';

injectTapEventPlugin();
let {Colors} = Styles;

class UserRecommendation extends React.Component {

    constructor(props) {
        super(props);
        this.openUserTwitterProfile = this.openUserTwitterProfile.bind(this);
    }

    openUserTwitterProfile() {
        window.open(`https://twitter.com/${this.props.username}`);
    }

    render() {
        return(
            <ListItem key={this.props.key}
                      leftAvatar={<Avatar src={`http://avatars.io/twitter/${this.props.username}`} />}
                      onTouchTap={this.openUserTwitterProfile}
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