import React from 'react';
import {Badge, Styles, Avatar, GridTile} from 'material-ui';
import injectTapEventPlugin from 'react-tap-event-plugin';

injectTapEventPlugin();
let {Colors} = Styles;

export default class UserRecommendation extends React.Component {

    constructor(props) {
        super(props);
        this.openUserTwitterProfile = this.openUserTwitterProfile.bind(this);
    }

    openUserTwitterProfile() {
        window.open(`https://twitter.com/${this.props.username}`);
    }

    render() {
        /**
         * TODO : Change to GridList with User avatar backgrounds
         */
        return (
            <GridTile key={this.props.key}
                      title={`@${this.props.username}`}
            >
                <img src={`http://avatars.io/twitter/${this.props.username}`} alt={this.props.username}/>
            </GridTile>
        )
    }
}
