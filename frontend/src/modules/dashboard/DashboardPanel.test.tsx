import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DashboardPanel } from './DashboardPanel';

describe('DashboardPanel', () => {
  it('renders metrics and resource popularity cards', () => {
    const onReload = vi.fn();

    render(
      <DashboardPanel
        overview={{
          totalBookings: 12,
          approvedBookings: 7,
          pendingApprovals: 2,
          waitlistedBookings: 1,
          cancelledOrRejectedBookings: 2,
          uniqueResourcesUsed: 5,
          nextSevenDaysApprovedBookings: 3,
          totalApprovedReservedMinutes: 840
        }}
        popularResources={[
          {
            resourceId: 'resource-1',
            resourceName: 'Room A',
            resourceType: 'ROOM',
            totalBookings: 5,
            approvedBookings: 4,
            pendingBookings: 1,
            waitlistedBookings: 0,
            cancelledBookings: 0,
            totalReservedMinutes: 240,
            popularityScore: 97.5,
            lastRefreshedAt: '2026-06-01T10:00:00Z'
          }
        ]}
        onReload={onReload}
      />
    );

    expect(screen.getByText('Total bookings')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Room A')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Reload' }));
    expect(onReload).toHaveBeenCalledTimes(1);
  });
});
