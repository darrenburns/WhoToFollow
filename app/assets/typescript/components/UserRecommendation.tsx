import * as React from 'react';
import {Link} from 'react-router';
import {Styles, Avatar, GridTile} from 'material-ui';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import * as Immutable from 'immutable';
import ReactElement = __React.ReactElement;

let {Colors} = Styles;

interface UserRecommendationProps {
    key: string,
    query: string,
    screenName: string,
    name: string,
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
                        <img src={`https://twitter.com/${this.props.screenName}/profile_image?size=original`}
                             alt={this.props.screenName} width="100px" height="100px"/>
                    </div>

                    <div className="recommendation-body">

                        <div className="recommendation-topline">
                            <span className="recommendation-name">
                                {this.props.name}
                            </span>
                            <Link to={`/query/${this.props.query}/user/${this.props.screenName}`}>
                                <span className="recommendation-username">@{this.props.screenName}</span>
                            </Link>
                        </div>

                        <div className="recommendation-bottomline">
                            <Sparklines data={correctlyOrderedHist} limit={50} width={150} height={50}>
                                <SparklinesLine style={{ strokeWidth: 1, stroke: "#41c3f9", fill: "none" }} />
                                <SparklinesSpots style={{ fill: "#41c3f9" }} />
                            </Sparklines>
                            <span className="recommendation-score">
                                {`${this.props.score.toFixed(2)}`}
                            </span>
                        </div>

                    </div>
                </div>
        )
    }
}
