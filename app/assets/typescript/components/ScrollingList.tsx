import * as React from 'react';
import * as moment from 'moment';
import {VelocityTransitionGroup, velocityHelpers} from 'velocity-react';
import 'velocity-animate/velocity.ui';
import {RecentQuery} from "./Home";


interface ScrollingListState {
    items: Array<RecentQuery>
}

interface ScrollingListProps {
    items: Array<RecentQuery>
    numItemsToShow: number
    duration: number
    children?: any
}

let defaultAnimations = {
    // Register these with UI Pack so that we can use stagger later.
    In: velocityHelpers.registerEffect({
        calls: [
            [{
                transformPerspective: [ 800, 800 ],
                transformOriginX: [ '50%', '50%' ],
                transformOriginY: [ '100%', '100%' ],
                marginBottom: 0,
                opacity: 1,
                rotateX: [0, 130],
            }, 1, {
                easing: 'ease-out',
                display: 'flex',
            }]
        ],
    }),

    Out: velocityHelpers.registerEffect({
        calls: [
            [{
                transformPerspective: [ 800, 800 ],
                transformOriginX: [ '50%', '50%' ],
                transformOriginY: [ '0%', '0%' ],
                marginBottom: -30,
                opacity: 0,
                rotateX: -70,
            }, 1, {
                easing: 'ease-out',
                display: 'flex',
            }]
        ],
    }),
};


export class ScrollingList extends React.Component<ScrollingListProps, ScrollingListState> {

    constructor(props) {
        super(props);
        this.state = {
            items: []
        }
    }

    componentWillReceiveProps(nextProps: ScrollingListProps): void {
        this.setState({
            items: nextProps.items
        })
    }


    render() {

        let enterAnimation = {
            animation: defaultAnimations.In,
            stagger: this.props.duration,
            duration: this.props.duration,
            backwards: true,
            display: 'flex',
            style: {
                display: 'none',
            },
        };

        let leaveAnimation = {
            animation: defaultAnimations.Out,
            stagger: this.props.duration,
            duration: this.props.duration,
            backwards: true,
        };

        let rows: Array<JSX.Element> = this.state.items.map((rq: RecentQuery, idx: number) =>
            <ScrollingListItem key={rq.id + ":" + rq.timestamp.toString()} text={rq.query} subtext={moment(rq.timestamp).fromNow()} />);

        return (
            <VelocityTransitionGroup component="div" className="scrolling-list" enter={enterAnimation} leave={leaveAnimation}>
                {rows}
            </VelocityTransitionGroup>
        )
    }

}

interface ScrollingListItemProps {
    key?: any
    text: string
    subtext: string
}

export class ScrollingListItem extends React.Component<ScrollingListItemProps, any> {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="scrolling-list-item" key={this.props.key}>
                <span className="scrolling-list-item-text">{this.props.text}</span>
                <span className="scrolling-list-item-subtext">{this.props.subtext}</span>
            </div>
        )
    }

}