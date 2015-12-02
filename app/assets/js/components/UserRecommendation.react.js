import React from 'react';
import {Styles, Avatar, GridTile} from 'material-ui';
import injectTapEventPlugin from 'react-tap-event-plugin';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import Immutable from 'immutable';

injectTapEventPlugin();
let {Colors} = Styles;

export default class UserRecommendation extends React.Component {

    constructor(props) {
        super(props);
        this.openUserTwitterProfile = this.openUserTwitterProfile.bind(this);
    }

    componentWillReceiveProps(props) {

        console.log("Received props");
    }

    openUserTwitterProfile() {
        window.open(`https://twitter.com/${this.props.username}`);
    }

    render = () => {
        let correctlyOrderedHist = this.props.userHistory.toArray();
        correctlyOrderedHist.reverse();
        return (
                <GridTile key={this.props.key}
                          title={` @${this.props.username}`}
                          subtitle={
                              <span>
                              {`Mentioned '${this.props.query}' ${this.props.rating} times`}
                              <br/>
                                  <Sparklines data={correctlyOrderedHist} limit={100} width={187}>
                                    <SparklinesLine style={{ strokeWidth: 1, stroke: "#41c3f9", fill: "none" }} />
                                    <SparklinesSpots style={{ fill: "#41c3f9" }} />
                                  </Sparklines>
                              </span>
                          }
                          style={{width: 320, height: 640, overflowY: 'auto'}}
                          onTouchTap={this.openUserTwitterProfile}
                          style={{overflowY: 'auto'}}
                >
                    <img src={`http://avatars.io/twitter/${this.props.username}`} alt={this.props.username}/>
                </GridTile>
        )
    }
}
