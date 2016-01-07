import * as React from 'react';
import {Link} from 'react-router';
import {Styles, Avatar, GridTile} from 'material-ui';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import * as Immutable from 'immutable';
import ReactElement = __React.ReactElement;

let {Colors} = Styles;

interface UserRecommendationProps {
    key: string,
    screenName: string,
    userHistory: Immutable.List<number>,
    score: number,
    params?: any
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
                <div className="recommendation-tile" key={this.props.key}>
                    <div className="recommendation-avatar">
                        <img src={`http://avatars.io/twitter/${this.props.screenName}`}
                             alt={this.props.screenName} width="80px" height="80px"/>
                    </div>
                    <div className="recommendation-middle">
                        <Link to={`/user/${this.props.screenName}`}>
                            <strong className="recommendation-username">@{this.props.screenName}</strong>
                        </Link>
                    </div>
                    <div className="recommendation-sparkline">
                        <Sparklines data={correctlyOrderedHist} limit={50} width={220} height={70}>
                            <SparklinesLine style={{ strokeWidth: 1, stroke: "#41c3f9", fill: "none" }} />
                            <SparklinesSpots style={{ fill: "#41c3f9" }} />
                        </Sparklines>
                    </div>
                    <div className="recommendation-score">
                        {`${this.props.score.toFixed(2)}`}
                    </div>
                </div>
        )
    }
}
