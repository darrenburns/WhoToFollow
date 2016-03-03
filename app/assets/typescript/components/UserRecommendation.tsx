import * as React from 'react';
import {Link} from 'react-router';
import {Styles, Avatar, GridTile} from 'material-ui';
import {Sparklines, SparklinesLine, SparklinesSpots} from 'react-sparklines';
import * as Immutable from 'immutable';
import ReactElement = __React.ReactElement;
import TwitterUtility from "../util/TwitterUtility";

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
    }

    render() {
        let correctlyOrderedHist: Array<number> = this.props.userHistory.toArray();
        return (
                <div className="recommendation-tile" key={this.props.key}>

                    <div className="recommendation-avatar">
                        <img src={TwitterUtility.getProfilePictureUrl(this.props.screenName, "original")}
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
                            <Sparklines data={correctlyOrderedHist} limit={100} width={140} height={46}>
                                <SparklinesLine style={{ strokeWidth: 1, stroke: "#0b97c2", fill: "#0b97c2", fillOpacity: ".3"}} />
                                <SparklinesSpots />
                            </Sparklines>
                            <span className="recommendation-score">
                                Score
                                <br/>
                                {`${this.props.score.toFixed(2)}`}
                            </span>
                        </div>

                    </div>
                </div>
        )
    }
}
