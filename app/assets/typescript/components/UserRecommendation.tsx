import * as React from 'react';
import {Styles, Avatar, GridTile} from 'material-ui';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import * as Immutable from 'immutable';
import ReactElement = __React.ReactElement;

let {Colors} = Styles;

interface UserRecommendationProps {
    key: string,
    username: string,
    query: string,
    userHistory: Immutable.List<number>,
    rating: number
}

export default class UserRecommendation extends React.Component<UserRecommendationProps, any> {

    constructor(props) {
        super(props);
        this.openUserTwitterProfile = this.openUserTwitterProfile.bind(this);
    }

    componentWillReceiveProps(props): void {

        console.log("Received props");
    }

    openUserTwitterProfile(): void {
        window.open(`https://twitter.com/${this.props.username}`);
    }

    render() {
        let correctlyOrderedHist: Array<number> = this.props.userHistory.toArray().reverse();
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
                >
                    <img src={`http://avatars.io/twitter/${this.props.username}`} alt={this.props.username}/>
                </GridTile>
        )
    }
}
