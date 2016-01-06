import * as React from 'react';
import {Styles, Avatar, GridTile} from 'material-ui';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import * as Immutable from 'immutable';
import ReactElement = __React.ReactElement;

let {Colors} = Styles;

interface UserRecommendationProps {
    key: string,
    screenName: string,
    userHistory: Immutable.List<number>,
    score: number
}

export default class UserRecommendation extends React.Component<UserRecommendationProps, any> {

    constructor(props) {
        super(props);
        this.openUserTwitterProfile = this.openUserTwitterProfile.bind(this);
    }

    openUserTwitterProfile(): void {
        window.open(`https://twitter.com/${this.props.screenName}`);
    }

    render() {
        let correctlyOrderedHist: Array<number> = this.props.userHistory.toArray();
        return (
                <GridTile key={this.props.key}
                          title={` @${this.props.screenName}`}
                          subtitle={
                              <span>
                              {`Score: '${this.props.score}'`}
                              <br/>
                                  <Sparklines data={correctlyOrderedHist} limit={50} width={187}>
                                    <SparklinesLine style={{ strokeWidth: 1, stroke: "#41c3f9", fill: "none" }} />
                                    <SparklinesSpots style={{ fill: "#41c3f9" }} />
                                  </Sparklines>
                                  <br/>
                              </span>
                          }
                >
                    <img src={`http://avatars.io/twitter/${this.props.screenName}`} alt={this.props.screenName}/>
                </GridTile>
        )
    }
}
