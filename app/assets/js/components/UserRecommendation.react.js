import React from 'react';
import {Styles, Avatar, GridTile} from 'material-ui';
import injectTapEventPlugin from 'react-tap-event-plugin';
import {Sparklines, SparklinesLine, SparklinesBars} from 'react-sparklines'

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
                          title={` @${this.props.username}`}
                          subtitle={
                          <span>
                          {`Mentioned '${this.props.query}' ${this.props.rating} times`}
                          <br/>
                              <Sparklines data={[10, 3, 14, 5, 8, 4,9,10,18,5,7,5]}>
                                <SparklinesBars style={{ fill: "#41c3f9", fillOpacity: ".5" }} />
                                <SparklinesLine style={{ strokeWidth: 2, stroke: "#41c3f9", fill: "none" }} />
                              </Sparklines>
                          </span>
                          }
                          style={{width: 320, height: 640, overflowY: 'auto'}}
                          onTouchTap={this.openUserTwitterProfile}
                          style={{overflowY: 'auto'}}
                          cellHeight={200}
                >
                    <img src={`http://avatars.io/twitter/${this.props.username}`} alt={this.props.username}/>
                </GridTile>
        )
    }
}
